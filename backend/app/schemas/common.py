from pydantic import BaseModel

class OK(BaseModel):
    ok: bool = True