import request from '@/utils/request'

export const captchaApi = {
  // 生成验证码
  generateCaptcha() {
    return request.get('/captcha/generate')
  },
  
  // 验证验证码
  verifyCaptcha(data) {
    return request.post('/captcha/verify', null, data)
  }
}
