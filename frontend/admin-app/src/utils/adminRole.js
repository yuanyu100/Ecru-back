import { authApi } from '../api/auth';

export const isAdminUser = (user) => {
  if (!user) {
    return false;
  }

  return user.role === 'ADMIN' || user.userId === 1 || user.id === 1;
};

export const getCurrentAdminFlag = () => isAdminUser(authApi.getCurrentUser());
