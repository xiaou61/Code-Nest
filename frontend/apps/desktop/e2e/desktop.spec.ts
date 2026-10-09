import { _electron as electron, expect, test } from '@playwright/test'

test('桌面端从源码启动后展示后端真实数据，并使用桌面平台实现', async () => {
  const app = await electron.launch({ args: ['.'] })
  try {
    const window = await app.firstWindow()

    // 平台实现必须是 desktop：说明组合根按构建模式选对了宿主
    await expect(window.getByTestId('platform-kind')).toHaveText('desktop')

    // 数据来自后端，不是硬编码
    await expect(window.getByTestId('health-status')).toHaveText('UP')
    await expect(window.getByTestId('health-raw')).toContainText('"status"')

    // 受保护接口在桌面端同样被拒（没有登录流程就没有令牌）
    await expect(window.getByTestId('identity-subject')).toHaveText('未登录')
  } finally {
    await app.close()
  }
})
