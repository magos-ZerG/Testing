from fastapi.routing import APIRoute
from starlette.requests import Request

from app.core.logging import bind_route_context, reset_route_context


class LoggedRoute(APIRoute):
    def get_route_handler(self):
        original_route_handler = super().get_route_handler()

        async def custom_route_handler(request: Request):
            route_name = self.name or self.path
            tokens = bind_route_context(route=route_name, path_params=request.path_params)
            try:
                return await original_route_handler(request)
            finally:
                reset_route_context(tokens)

        return custom_route_handler
