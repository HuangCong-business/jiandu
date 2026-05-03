// 简牍 - AI 分析 API (Supabase Edge Function)
// 功能：接收用户请求，转发到 DeepSeek API，记录使用量

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

serve(async (req) => {
  // 处理 CORS 预检请求
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  try {
    // ============================================
    // 1. 验证用户身份
    // ============================================
    const authHeader = req.headers.get('Authorization');
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: '缺少认证信息' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    // 从 Supabase 验证 token
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      {
        global: {
          headers: { Authorization: authHeader },
        },
      }
    );

    const { data: { user }, error: userError } = await supabaseClient.auth.getUser();
    
    if (userError || !user) {
      return new Response(
        JSON.stringify({ error: '用户认证失败', details: userError?.message }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    // ============================================
    // 2. 检查用户使用限额
    // ============================================
    const { data: usageCheck, error: checkError } = await supabaseClient
      .rpc('can_user_use', { target_user_id: user.id });
    
    if (checkError) {
      console.error('使用限额检查失败:', checkError);
    }

    if (usageCheck && usageCheck.length > 0) {
      const { can_use, reason, remaining } = usageCheck[0];
      if (!can_use) {
        return new Response(
          JSON.stringify({ 
            error: reason,
            remaining: remaining,
            upgrade_hint: '升级到专业版解锁无限分析'
          }),
          { status: 429, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
      }
    }

    // ============================================
    // 3. 获取请求体
    // ============================================
    const { query, files, meta } = await req.json();
    
    if (!query || !files) {
      return new Response(
        JSON.stringify({ error: '缺少必要参数：query 和 files' }),
        { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    // ============================================
    // 4. 从数据库获取 DeepSeek API Key
    // ============================================
    const { data: configData, error: configError } = await supabaseClient
      .from('admin_config')
      .select('config_value')
      .eq('config_key', 'deepseek_api_key')
      .single();
    
    if (configError || !configData?.config_value) {
      return new Response(
        JSON.stringify({ 
          error: '管理员未配置 API Key',
          hint: '请联系管理员在后台配置 DeepSeek API Key'
        }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    const DEEPSEEK_API_KEY = configData.config_value;

    // ============================================
    // 5. 构建 AI 提示词
    // ============================================
    const systemPrompt = `你是一个数据分析专家，擅长使用 Python 的 pandas 和 matplotlib 处理 Excel 和 CSV 文件。

重要规则：
1. 绝对不能用 pd.read_excel() 读取 CSV 文件！CSV 文件必须用 read_csv()
2. 使用 find_real_data() 函数读取 Excel 文件（自动处理非 A1 表头）
3. 使用 read_csv() 函数读取 CSV 文件（支持多种编码）
4. 生成的代码必须完整可执行
5. 输出必须是有效的 JSON 格式

文件类型说明：
- 类型:EXCEL - 使用 find_real_data("文件名.xlsx")
- 类型:CSV - 使用 read_csv("文件名.csv")

请根据用户需求生成 Python 代码，返回以下 JSON 格式：
{
  "code": "完整的 Python 代码",
  "needs_chart": true/false,
  "description": "代码功能说明"
}`;

    const fileMeta = files.map((f: any) => {
      return `文件名：${f.name}, 类型:${f.type}, 行数:${f.rows}, 列:${f.columns?.join(',')}`;
    }).join('\n');

    const userPrompt = `用户查询：${query}

文件信息：
${fileMeta}

请生成 Python 代码来处理这个分析任务。`;

    // ============================================
    // 6. 调用 DeepSeek API
    // ============================================
    const deepSeekResponse = await fetch('https://api.deepseek.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${DEEPSEEK_API_KEY}`,
      },
      body: JSON.stringify({
        model: 'deepseek-chat',
        messages: [
          { role: 'system', content: systemPrompt },
          { role: 'user', content: userPrompt }
        ],
        temperature: 0.3,
        max_tokens: 2000,
      }),
    });

    if (!deepSeekResponse.ok) {
      const errorText = await deepSeekResponse.text();
      console.error('DeepSeek API 错误:', errorText);
      
      return new Response(
        JSON.stringify({ 
          error: 'AI 服务响应异常',
          status: deepSeekResponse.status,
          details: errorText.substring(0, 200)
        }),
        { status: deepSeekResponse.status, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    const deepSeekData = await deepSeekResponse.json();
    const aiContent = deepSeekData.choices[0]?.message?.content;
    const tokensUsed = deepSeekData.usage?.total_tokens || 0;

    // ============================================
    // 7. 解析 AI 响应
    // ============================================
    let plan;
    try {
      // 尝试从 AI 响应中提取 JSON
      const jsonMatch = aiContent.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        plan = JSON.parse(jsonMatch[0]);
      } else {
        plan = { code: aiContent, needs_chart: false, description: 'AI 生成的代码' };
      }
    } catch (e) {
      plan = { code: aiContent, needs_chart: false, description: 'AI 生成的代码' };
    }

    // ============================================
    // 8. 记录使用量
    // ============================================
    await supabaseClient.rpc('record_usage', {
      target_user_id: user.id,
      target_email: user.email,
      p_tokens: tokensUsed,
      p_file_count: files.length,
      p_request_type: 'analyze'
    });

    // ============================================
    // 9. 返回结果
    // ============================================
    return new Response(
      JSON.stringify({
        success: true,
        plan: plan,
        tokens_used: tokensUsed,
        usage: {
          daily_remaining: usageCheck?.[0]?.remaining || 'unknown'
        }
      }),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    );

  } catch (error) {
    console.error('分析 API 错误:', error);
    return new Response(
      JSON.stringify({ 
        error: '服务器内部错误',
        details: error.message 
      }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    );
  }
});
