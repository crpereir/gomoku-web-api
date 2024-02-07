import * as React from 'react';
import { createBrowserRouter, Outlet, RouterProvider } from 'react-router-dom';
import { RequireAuthn } from './utils/auth/RequireAuthn';
import { Home } from './pages/Home';
import { Login } from './pages/Login';
import { SignUp } from './pages/SignUp';
import { Ranking } from './pages/Ranking';
import { Me } from './pages/Me';
import { Game } from './pages/Game';
import { UserStatistics } from './pages/UserStatistics';

const router = createBrowserRouter([
  {
    path: '/',
    element: <Outlet />,
    children: [
      {
        path: '/',
        element: <Home />,
      },
      {
        path: '/login',
        element: <Login />,
      },
      {
        path: '/signup',
        element: <SignUp />,
      },
      {
        path: '/ranking',
        element: <Ranking />,
      },
      {
        path: '/users/:uid',
        element: <UserStatistics />,
      },
      {
        path: '/me',
        element: (
          <RequireAuthn>
            <Me />
          </RequireAuthn>
        ),
      },
      {
        path: '/games/:gid',
        element: (
          <RequireAuthn>
            <Game />
          </RequireAuthn>
        ),
      },
    ],
  },
]);

export function App() {
  return <RouterProvider router={router} />;
}
