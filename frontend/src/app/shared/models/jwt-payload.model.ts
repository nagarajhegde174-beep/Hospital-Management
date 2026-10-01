export interface JwtPayload {
  sub: string;
  userId: number;
  serviceId: number;
  role: string;
  iat: number;
  exp: number;
}
