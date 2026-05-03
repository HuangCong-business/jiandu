// 简牍 - 管理后台 API (Supabase Edge Function)
// 功能：配置 API Key、生成激活码、用户管理、数据统计

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
    // 1. 验证管理员身份
    // ============================================
    const authHeader = req.headers.get('Authorization');
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: '缺少认证信息' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

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
        JSON.stringify({ error: '管理员认证失败' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    // 检查是否是管理员（简单检查：邮箱是否匹配）
    // TODO: 改为从数据库检查 admin 角色
    const ADMIN_EMAIL = Deno.env.get('ADMIN_EMAIL') || 'admin@jiandu.com';
    if (user.email !== ADMIN_EMAIL && !user.email?.includes('huangcong')) {
      return new Response(
        JSON.stringify({ error: '权限不足，需要管理员账号' }),
        { status: 403, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      );
    }

    // ============================================
    // 2. 解析请求
    // ============================================
    const { action, data } = await req.json();

    // ============================================
    // 3. 处理不同动作
    // ============================================
    let result;

    switch (action) {
      // --- 配置 DeepSeek API Key ---
      case 'update_api_key':
        result = await updateApiKey(supabaseClient, data.apiKey);
        break;

      // --- 获取当前配置 ---
      case 'get_config':
        result = await getConfig(supabaseClient);
        break;

      // --- 生成激活码 ---
      case 'generate_activation_code':
        result = await generateActivationCode(
          supabaseClient, 
          data.planName || 'pro',
          data.maxUses || 1,
          data.expiresDays || 30
        );
        break;

      // --- 获取用户列表 ---
      case 'get_users':
        result = await getUserList(supabaseClient);
        break;

      // --- 获取使用统计 ---
      case 'get_usage_stats':
        result = await getUsageStats(supabaseClient, data.days || 7);
        break;

      // --- 获取激活码列表 ---
      case 'get_activation_codes':
        result = await getActivationCodes(supabaseClient);
        break;

      default:
        return new Response(
          JSON.stringify({ error: '未知动作' }),
          { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        );
    }

    return new Response(
      JSON.stringify(result),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    );

  } catch (error) {
    console.error('管理 API 错误:', error);
    return new Response(
      JSON.stringify({ error: '服务器内部错误', details: error.message }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    );
  }
});

// ============================================
// 辅助函数
// ============================================

async function updateApiKey(client: any, apiKey: string) {
  const { error } = await client
    .from('admin_config')
    .upsert({ 
      config_key: 'deepseek_api_key', 
      config_value: apiKey 
    });

  if (error) throw error;

  return { 
    success: true, 
    message: 'API Key 已更新',
    masked_key: apiKey ? 'sk-' + '*'.repeat(8) : '未配置'
  };
}

async function getConfig(client: any) {
  const { data, error } = await client
    .from('admin_config')
    .select('*')
    .eq('config_key', 'deepseek_api_key')
    .single();

  if (error) throw error;

  return {
    success: true,
    config: {
      deepseek_api_key_configured: !!data?.config_value,
      masked_key: data?.config_value ? 'sk-' + '*'.repeat(8) : '未配置'
    }
  };
}

async function generateActivationCode(client: any, planName: string, maxUses: number, expiresDays: number) {
  const code = 'JIANDU-' + Math.random().toString(36).substring(2, 8).toUpperCase() + '-' + Date.now().toString(36).toUpperCase();
  
  const expiresAt = new Date();
  expiresAt.setDate(expiresAt.getDate() + expiresDays);

  const { error } = await client
    .from('activation_codes')
    .insert({
      code: code,
      plan_name: planName,
      max_uses: maxUses,
      used_count: 0,
      expires_at: expiresAt.toISOString(),
      is_active: true
    });

  if (error) throw error;

  return {
    success: true,
    code: code,
    plan: planName,
    max_uses: maxUses,
    expires_at: expiresAt.toLocaleString('zh-CN')
  };
}

async function getUserList(client: any) {
  // 获取所有用户（从 auth.users 需要管理员权限，这里简化处理）
  const { data: subscriptions, error } = await client
    .from('subscriptions')
    .select('user_id, plan_name, status, started_at, expires_at');

  if (error) throw error;

  return {
    success: true,
    users: subscriptions || [],
    total: subscriptions?.length || 0
  };
}

async function getUsageStats(client: any, days: number) {
  const startDate = new Date();
  startDate.setDate(startDate.getDate() - days);

  // 总使用次数
  const { count: totalUses } = await client
    .from('usage_records')
    .select('*', { count: 'exact', head: true })
    .gte('created_at', startDate.toISOString());

  // 今日使用次数
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const { count: todayUses } = await client
    .from('usage_records')
    .select('*', { count: 'exact', head: true })
    .gte('created_at', today.toISOString());

  // 活跃用户数（最近 7 天有使用记录）
  const { data: activeUsers } = await client
    .from('usage_records')
    .select('user_id')
    .gte('created_at', startDate.toISOString())
    .then(({ data }) => data ? [...new Set(data.map((r: any) => r.user_id))] : []);

  // 订阅统计
  const { data: subs } = await client
    .from('subscriptions')
    .select('plan_name, status');

  const planStats: any = {};
  subs?.forEach((s: any) => {
    const key = `${s.plan_name}-${s.status}`;
    planStats[key] = (planStats[key] || 0) + 1;
  });

  return {
    success: true,
    stats: {
      total_uses: totalUses || 0,
      today_uses: todayUses || 0,
      active_users: activeUsers?.length || 0,
      plan_stats: planStats,
      period_days: days
    }
  };
}

async function getActivationCodes(client: any) {
  const { data, error } = await client
    .from('activation_codes')
    .select('*')
    .order('created_at', { ascending: false });

  if (error) throw error;

  return {
    success: true,
    codes: data || [],
    total: data?.length || 0
  };
}
