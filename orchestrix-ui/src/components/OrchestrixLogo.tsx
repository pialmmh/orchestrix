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
      <defs>
        <linearGradient id="petalGradient1" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" style={{ stopColor: '#ff6ec7', stopOpacity: 1 }} />
          <stop offset="100%" style={{ stopColor: '#c471ed', stopOpacity: 1 }} />
        </linearGradient>
        <linearGradient id="petalGradient2" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" style={{ stopColor: '#ffa3d5', stopOpacity: 1 }} />
          <stop offset="100%" style={{ stopColor: '#e589f5', stopOpacity: 1 }} />
        </linearGradient>
        <linearGradient id="petalGradient3" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" style={{ stopColor: '#ffb3e6', stopOpacity: 1 }} />
          <stop offset="100%" style={{ stopColor: '#d4a5f9', stopOpacity: 1 }} />
        </linearGradient>
        <radialGradient id="centerGradient">
          <stop offset="0%" style={{ stopColor: '#ffd700', stopOpacity: 1 }} />
          <stop offset="70%" style={{ stopColor: '#ffaa00', stopOpacity: 1 }} />
          <stop offset="100%" style={{ stopColor: '#ff8c00', stopOpacity: 1 }} />
        </radialGradient>
        <filter id="softGlow">
          <feGaussianBlur stdDeviation="2" result="coloredBlur"/>
          <feMerge>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      
      {/* Top petal */}
      <ellipse cx="50" cy="25" rx="18" ry="23" fill="url(#petalGradient1)" transform="rotate(-5 50 50)" opacity="0.95"/>
      
      {/* Top-right petal */}
      <ellipse cx="70" cy="35" rx="16" ry="22" fill="url(#petalGradient2)" transform="rotate(35 50 50)" opacity="0.9"/>
      
      {/* Right petal */}
      <ellipse cx="75" cy="50" rx="18" ry="23" fill="url(#petalGradient3)" transform="rotate(75 50 50)" opacity="0.95"/>
      
      {/* Bottom-right petal */}
      <ellipse cx="65" cy="70" rx="16" ry="21" fill="url(#petalGradient1)" transform="rotate(115 50 50)" opacity="0.9"/>
      
      {/* Bottom petal */}
      <ellipse cx="50" cy="75" rx="18" ry="23" fill="url(#petalGradient2)" transform="rotate(175 50 50)" opacity="0.95"/>
      
      {/* Bottom-left petal */}
      <ellipse cx="35" cy="70" rx="16" ry="21" fill="url(#petalGradient3)" transform="rotate(215 50 50)" opacity="0.9"/>
      
      {/* Left petal */}
      <ellipse cx="25" cy="50" rx="18" ry="23" fill="url(#petalGradient1)" transform="rotate(255 50 50)" opacity="0.95"/>
      
      {/* Top-left petal */}
      <ellipse cx="30" cy="35" rx="16" ry="22" fill="url(#petalGradient2)" transform="rotate(325 50 50)" opacity="0.9"/>
      
      {/* Inner petals layer for depth */}
      <ellipse cx="50" cy="35" rx="12" ry="16" fill="url(#petalGradient3)" transform="rotate(15 50 50)" opacity="0.7"/>
      <ellipse cx="65" cy="50" rx="12" ry="16" fill="url(#petalGradient1)" transform="rotate(90 50 50)" opacity="0.7"/>
      <ellipse cx="50" cy="65" rx="12" ry="16" fill="url(#petalGradient2)" transform="rotate(165 50 50)" opacity="0.7"/>
      <ellipse cx="35" cy="50" rx="12" ry="16" fill="url(#petalGradient3)" transform="rotate(270 50 50)" opacity="0.7"/>
      
      {/* Center of the orchid */}
      <circle cx="50" cy="50" r="12" fill="url(#centerGradient)" filter="url(#softGlow)"/>
      
      {/* Center details */}
      <circle cx="47" cy="48" r="2" fill="#fff" opacity="0.8" />
      <circle cx="53" cy="52" r="1.5" fill="#fff" opacity="0.6" />
    </SvgIcon>
  );
};

export default OrchestrixLogo;