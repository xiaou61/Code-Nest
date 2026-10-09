/**
 * 会话与角色端口。
 *
 * <p>前端需要一个地方判断"当前是谁、能不能进管理端"，而这样的判断只能有一处。
 * 业务组件不得自己比较角色字符串——否则将来加第三种角色或换认证机制时，要逐处改调用点。
 *
 * <p>**这不是授权边界。** 客户端解析 JWT 载荷时不校验签名（没有密钥也校验不了），
 * 任何人都能伪造一个载荷。它只用于界面分流：让不该看到管理入口的人不看到，
 * 让越权请求早一点被拦下来，省一次往返。真正的拦截必须在后端。
 */

/** 本期只有两种角色。 */
export type Role = 'admin' | 'learner'

export interface Session {
  readonly role: Role
  readonly signedIn: true
}

/** 身份来源端口。后端角色契约落地后，只需换掉传给 `createSessionReader` 的 `getToken`。 */
export interface SessionReader {
  current(): Session | null
}

const KNOWN_ROLES: readonly Role[] = ['admin', 'learner']

function isRole(value: unknown): value is Role {
  return typeof value === 'string' && (KNOWN_ROLES as readonly string[]).includes(value)
}

/**
 * 取 JWT 的第二段（载荷）并解析。
 *
 * <p>只做 base64url 解码与 JSON 解析，**不校验签名**（见文件头说明）。
 * 任何一步失败都返回 `null`，由调用方按"未登录"处理。
 */
function decodePayload(token: string): Record<string, unknown> | null {
  const segments = token.split('.')
  if (segments.length < 3) {
    return null
  }
  const payload = segments[1]
  if (payload === undefined || payload === '') {
    return null
  }
  try {
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    const json = atob(padded)
    const parsed: unknown = JSON.parse(json)
    return typeof parsed === 'object' && parsed !== null ? (parsed as Record<string, unknown>) : null
  } catch {
    return null
  }
}

export function createSessionReader(options: { getToken?: () => string | null } = {}): SessionReader {
  return {
    current(): Session | null {
      const token = options.getToken?.() ?? null
      if (token === null || token === '') {
        return null
      }
      const payload = decodePayload(token)
      if (payload === null) {
        return null
      }
      // 失败即拒绝：令牌里没有角色、或角色不是已知的两种，一律按最小权限的学习者处理，
      // 而不是放行或抛错。后端将来只签发两种角色，认不出的值只可能来自伪造或升级不同步。
      const role = payload['role']
      return { role: isRole(role) ? role : 'learner', signedIn: true }
    },
  }
}

/** 角色判断的唯一入口。 */
export function canAccessAdmin(session: Session | null): boolean {
  return session?.role === 'admin'
}
