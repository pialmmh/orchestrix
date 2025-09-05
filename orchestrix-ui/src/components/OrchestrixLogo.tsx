import React from 'react';
import { SvgIcon } from '@mui/material';

interface OrchestrixLogoProps {
  sx?: any;
  fontSize?: string | number;
}

const OrchestrixLogo: React.FC<OrchestrixLogoProps> = ({ sx, fontSize = 'medium', ...props }) => {
  return (
    <SvgIcon 
      sx={{ 
        ...sx,
        width: fontSize === 'medium' ? 24 : fontSize === 'large' ? 32 : fontSize,
        height: fontSize === 'medium' ? 24 : fontSize === 'large' ? 32 : fontSize,
      }} 
      viewBox="0 0 100 100" 
      {...props}
    >
      {/* Modern hexagonal nodes representing distributed systems */}
      
      {/* Central hub hexagon */}
      <path d="M50 25 L62 35 L62 55 L50 65 L38 55 L38 35 Z" 
            fill="#1976d2" stroke="#ffffff" strokeWidth="2"/>
      
      {/* Top left hexagon */}
      <path d="M25 15 L32 20 L32 30 L25 35 L18 30 L18 20 Z" 
            fill="#42a5f5" stroke="#ffffff" strokeWidth="1.5"/>
      
      {/* Top right hexagon */}
      <path d="M75 15 L82 20 L82 30 L75 35 L68 30 L68 20 Z" 
            fill="#42a5f5" stroke="#ffffff" strokeWidth="1.5"/>
      
      {/* Bottom left hexagon */}
      <path d="M25 65 L32 70 L32 80 L25 85 L18 80 L18 70 Z" 
            fill="#42a5f5" stroke="#ffffff" strokeWidth="1.5"/>
      
      {/* Bottom right hexagon */}
      <path d="M75 65 L82 70 L82 80 L75 85 L68 80 L68 70 Z" 
            fill="#42a5f5" stroke="#ffffff" strokeWidth="1.5"/>
      
      {/* Connection lines */}
      <line x1="38" y1="35" x2="25" y2="30" stroke="#1976d2" strokeWidth="2" opacity="0.7"/>
      <line x1="62" y1="35" x2="75" y2="30" stroke="#1976d2" strokeWidth="2" opacity="0.7"/>
      <line x1="38" y1="55" x2="25" y2="70" stroke="#1976d2" strokeWidth="2" opacity="0.7"/>
      <line x1="62" y1="55" x2="75" y2="70" stroke="#1976d2" strokeWidth="2" opacity="0.7"/>
      
      {/* Center dot */}
      <circle cx="50" cy="45" r="3" fill="#ffffff"/>
      
      {/* Small dots on outer nodes */}
      <circle cx="25" cy="25" r="1.5" fill="#ffffff"/>
      <circle cx="75" cy="25" r="1.5" fill="#ffffff"/>
      <circle cx="25" cy="75" r="1.5" fill="#ffffff"/>
      <circle cx="75" cy="75" r="1.5" fill="#ffffff"/>
    </SvgIcon>
  );
};

export default OrchestrixLogo;