from fastapi import APIRouter, Depends

from app.api.route_logging import LoggedRoute
from app.models.user import User
from app.schemas.auth import LoginRequest, RefreshRequest, RegisterRequest, TokenPairResponse, UserResponse
from app.services.auth_service import AuthService
from app.services.deps import get_auth_service, get_current_user

router = APIRouter(prefix="/api/v1/auth", tags=["auth"], route_class=LoggedRoute)


@router.post("/register", response_model=UserResponse)
def register(payload: RegisterRequest, auth: AuthService = Depends(get_auth_service)):
    user = auth.register_user(payload)
    return UserResponse(id=user.id, email=user.email, createdAt=user.created_at)


@router.post("/login", response_model=TokenPairResponse)
def login(payload: LoginRequest, auth: AuthService = Depends(get_auth_service)):
    return auth.login_user(payload)


@router.post("/refresh", response_model=TokenPairResponse)
def refresh(payload: RefreshRequest, auth: AuthService = Depends(get_auth_service)):
    return auth.refresh_access_token(payload.refreshToken)


@router.post("/logout")
def logout(payload: RefreshRequest, auth: AuthService = Depends(get_auth_service)):
    auth.logout_by_refresh_token(payload.refreshToken)
    return {"status": "ok"}


@router.get("/me", response_model=UserResponse)
def me(current_user: User = Depends(get_current_user)):
    return UserResponse(id=str(current_user.id), email=str(current_user.email), createdAt=current_user.created_at)
