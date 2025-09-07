import React from 'react';
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { RootState } from '../store/store';

interface PrivateRouteProps {
  children: React.ReactNode;
}

const PrivateRoute: React.FC<PrivateRouteProps> = ({ children }) => {
  // Authentication is disabled in backend (all endpoints are permitAll)
  // So we always allow access to routes
  return <>{children}</>;
};

export default PrivateRoute;