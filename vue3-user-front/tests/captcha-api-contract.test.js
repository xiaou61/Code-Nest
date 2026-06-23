import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')
const captchaApiSource = readFileSync(resolve(projectRoot, 'src/api/captcha.js'), 'utf8')
const captchaControllerSource = readFileSync(
  resolve(projectRoot, '../xiaou-user/src/main/java/com/xiaou/user/controller/CaptchaController.java'),
  'utf8'
)

test('captcha verification should send captchaKey and captcha as request params', () => {
  assert.match(
    captchaControllerSource,
    /verifyCaptcha\(@RequestParam String captchaKey,\s*@RequestParam String captcha\)/
  )
  assert.match(
    captchaApiSource,
    /verifyCaptcha\(data\) \{\s*return request\.post\('\/captcha\/verify', null, data\)\s*\}/
  )
})
