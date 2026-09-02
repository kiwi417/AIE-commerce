import path from 'path'
import fs from 'fs'

/**
 * Vite SPA History Fallback 插件
 *
 * 问题：Vue Router history 模式下，浏览器刷新非根路径（如 /ai/eval）
 * 会向 Vite dev server 发 GET 请求，Vite 没有对应文件就返回 404。
 *
 * 解决：对于非 API、非 Vite 内部、非静态资源的 GET 请求，
 * 读取 index.html 通过 transformIndexHtml 正确注入 HMR 客户端后返回。
 *
 * 注意：不能简单用 req.url = '/index.html' + next()，
 * 那样会让 Vite 的 import-analysis 插件把 HTML 当 JS 解析导致 500。
 */
export default function createHistoryFallback() {
  return {
    name: 'history-fallback',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = req.url

        // 排除：API 代理
        if (url.startsWith('/dev-api') || url.startsWith('/v3/api-docs')) {
          return next()
        }

        // 排除：Vite 内部路径（/@vite/client、/@fs/、/@id/ 等）
        if (url.startsWith('/@')) {
          return next()
        }

        // 排除：带后缀的静态资源（.js / .css / .png / .ico 等）
        const pathname = url.split('?')[0].split('#')[0]
        if (pathname.includes('.') && !pathname.endsWith('/')) {
          return next()
        }

        // SPA fallback：读取 index.html，用 Vite 的 transformIndexHtml
        // 注入 HMR 客户端、环境变量等，然后返回
        const htmlPath = path.resolve(process.cwd(), 'index.html')
        const rawHtml = fs.readFileSync(htmlPath, 'utf-8')
        server.transformIndexHtml(url, rawHtml)
          .then(transformed => {
            res.statusCode = 200
            res.setHeader('Content-Type', 'text/html; charset=utf-8')
            res.end(transformed)
          })
          .catch(err => {
            server.ssrFixStacktrace(err)
            next(err)
          })
      })
    }
  }
}
