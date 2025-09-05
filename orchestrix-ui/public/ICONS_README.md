# Orchestrix Icons

This directory contains the custom icons for the Orchestrix application.

## Icon Design

The Orchestrix icon represents container orchestration with:
- **Blue circular background** (#1976d2) - representing the platform
- **White container boxes** - representing LXC containers
- **Orange central hub** (#ff9800) - representing the orchestration engine
- **White connecting lines** - representing network connections between containers
- **Green dashed lines** - representing inter-container communication

## Icon Files

### Main Icons
- `orchestrix-icon.svg` - Full detailed icon (512x512 equivalent)
- `orchestrix-192.svg` - Medium size icon (192x192 equivalent) 
- `orchestrix-32.svg` - Small size icon (32x32 equivalent)
- `orchestrix-favicon.svg` - Ultra-simplified favicon version (16x16 equivalent)

### Legacy Support
- `favicon.ico` - Traditional ICO format (kept for fallback)
- `logo192.png` - Original React logo (can be removed)
- `logo512.png` - Original React logo (can be removed)

## Usage

The icons are automatically loaded through:
- `index.html` - Sets favicon and apple-touch-icon
- `manifest.json` - Defines PWA app icons

## Color Scheme

- **Primary Blue**: #1976d2 (Material Design Blue 700)
- **Dark Blue**: #0d47a1 (Material Design Blue 900) 
- **Orange**: #ff9800 (Material Design Orange 500)
- **Dark Orange**: #e65100 (Material Design Deep Orange 900)
- **White**: #ffffff
- **Light Blue**: #4fc3f7, #29b6f6, #03a9f4 (Container indicators)
- **Green**: #81c784 (Network connections)

## Icon Meaning

The icon symbolizes:
1. **Container Orchestration** - Multiple containers managed by a central hub
2. **Networking** - Interconnected systems with communication paths  
3. **Scalability** - Distributed architecture with multiple nodes
4. **Management** - Central control point (orange hub) coordinating all containers

Perfect for representing an LXC container orchestration platform like Orchestrix.