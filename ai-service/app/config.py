"""服务配置：从环境变量 / .env 读取（字段名的大写形式即环境变量名）。"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    deepseek_api_key: str
    deepseek_base_url: str = "https://api.deepseek.com"
    model: str = "deepseek-chat"
    host: str = "127.0.0.1"
    port: int = 8000
    session_ttl_seconds: int = 1800
    classify_timeout_seconds: float = 5.0
    answer_timeout_seconds: float = 60.0

    # Redis（会话历史 + 检索上下文）
    redis_host: str = "127.0.0.1"
    redis_port: int = 6379
    redis_db: int = 0
    redis_password: str = ""
    redis_session_ttl_seconds: int = 1800

    # 向量知识库
    embedding_model: str = "BAAI/bge-small-zh-v1.5"
    hf_home: str = ""  # HuggingFace 缓存目录（为空则用系统默认）
    hf_endpoint: str = ""  # HuggingFace 镜像端点（为空则直连）
    vector_backend: str = "faiss"  # faiss | numpy（faiss 不可用时自动回退）
    data_dir: str = "./data"
    kb_refresh_interval_seconds: int = 600
    retrieval_top_k: int = 4
    retrieval_score_threshold: float = 0.35

    # Java 服务（商品数据源）
    java_base_url: str = "http://127.0.0.1:8080"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


@lru_cache
def get_settings() -> Settings:
    return Settings()
