"""启动入口：python -m app.main（等价于 uvicorn app.api:app）。"""

import uvicorn

from app.config import get_settings

if __name__ == "__main__":
    settings = get_settings()
    key = settings.deepseek_api_key or ""
    if not key.startswith("sk-") or "你的key" in key:
        print("警告：DEEPSEEK_API_KEY 疑似未填写真实值，请在 .env 中填写后重启服务")
    uvicorn.run("app.api:app", host=settings.host, port=settings.port)
