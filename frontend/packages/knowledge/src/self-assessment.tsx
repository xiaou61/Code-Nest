import type { SelfAssessmentLevel } from './types'

const OPTIONS: readonly { level: SelfAssessmentLevel; label: string }[] = [
  { level: 'understood', label: '懂了' },
  { level: 'unsure', label: '还不懂' },
]

/**
 * 自评按钮。
 *
 * <p>再点一次同一个按钮就是**取消标记**——不额外放一个"清除"，因为用户想撤销时最自然的
 * 动作就是再点一下它。取消是幂等的，重复请求不会报错。
 *
 * <p>刻意只有两个状态：这是学习者模型的第一块砖，不是掌握度推断。做出"五档熟练度"会让人
 * 以为系统真的算出了什么，而它其实只是听用户说。
 */
export function SelfAssessmentButtons({
  current,
  disabled,
  onChange,
}: {
  current: SelfAssessmentLevel | null
  disabled?: boolean
  onChange: (next: SelfAssessmentLevel | null) => void
}) {
  return (
    <div className="border-border flex flex-wrap items-center gap-2 rounded-md border p-3" data-testid="self-assessment">
      <span className="text-muted-foreground text-xs">这条内容你掌握了吗</span>
      {OPTIONS.map((option) => {
        const active = current === option.level
        return (
          <button
            key={option.level}
            type="button"
            disabled={disabled === true}
            aria-pressed={active}
            data-testid={`self-assessment-${option.level}`}
            onClick={() => onChange(active ? null : option.level)}
            className={
              active
                ? 'border-foreground bg-muted rounded-md border px-3 py-1 text-xs font-medium'
                : 'border-border text-muted-foreground hover:text-foreground rounded-md border px-3 py-1 text-xs'
            }
          >
            {option.label}
          </button>
        )
      })}
      {current === null ? null : (
        <span className="text-muted-foreground text-xs">（再点一次可取消）</span>
      )}
    </div>
  )
}
