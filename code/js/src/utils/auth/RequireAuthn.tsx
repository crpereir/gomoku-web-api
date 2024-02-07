import * as React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { getCookie } from './Authn';

export function RequireAuthn({ children }: { children: React.ReactNode }): React.ReactElement {
  const cookie = getCookie('login');
  const location = useLocation();

  if (cookie) {
    return <>{children}</>;
  } else {
    return <Navigate to="/login" state={{ source: location.pathname }} replace={true} />;
  }
}
