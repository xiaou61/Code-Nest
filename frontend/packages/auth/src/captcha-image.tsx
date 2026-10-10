import { Button, Skeleton } from '@paideia/ui'

import type { CaptchaChallenge } from './auth-api'

/**
 * 图形验证码。它保护的是"发邮箱验证码"这个接口，不是登录。
 *
 * <p>图是后端出的 base64 PNG，点击可以换一张——用户看不清时不必刷新整个页面。
 */
export function CaptchaImage({
  challenge,
  onRefresh,
  disabled = false,
}: {
  challenge: CaptchaChallenge | null
  onRefresh: () => void
  disabled?: boolean
}) {
  return (
    <div className="flex items-center gap-2">
      {challenge === null ? (
        <Skeleton className="h-10 w-30" />
      ) : (
        <img
          data-testid="captcha-image"
          src={`data:image/png;base64,${challenge.imageBase64}`}
          alt="图形验证码"
          className="border-border h-10 w-30 rounded-md border"
        />
      )}
      <Button
        type="button"
        variant="ghost"
        size="sm"
        onClick={onRefresh}
        disabled={disabled}
        data-testid="captcha-refresh"
      >
        换一张
      </Button>
    </div>
  )
}
