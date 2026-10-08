from datetime import datetime
from pydantic import BaseModel, EmailStr, Field


class RegisterRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)


class LoginRequest(BaseModel):
    email: EmailStr
    password: str = Field(min_length=4, max_length=128)


class RefreshRequest(BaseModel):
    refreshToken: str


class TokenPairResponse(BaseModel):
    accessToken: str
    refreshToken: str
    tokenType: str = "bearer"


class UserResponse(BaseModel):
    id: str
    email: EmailStr
    createdAt: datetime
