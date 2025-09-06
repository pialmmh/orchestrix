#!/usr/bin/env python3
from PIL import Image, ImageDraw

# Create a simple orchid-inspired favicon
def create_orchid_favicon():
    # Create a 32x32 image with transparent background
    img = Image.new('RGBA', (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Draw simplified orchid petals (circles in pink/purple)
    # Center petal positions
    petals = [
        (16, 8, '#ff6ec7'),   # top
        (24, 16, '#e589f5'),  # right
        (16, 24, '#ff6ec7'),  # bottom
        (8, 16, '#e589f5'),   # left
        (22, 10, '#ffa3d5'),  # top-right
        (22, 22, '#d4a5f9'),  # bottom-right
        (10, 22, '#ffa3d5'),  # bottom-left
        (10, 10, '#d4a5f9'),  # top-left
    ]
    
    # Draw petals as ellipses
    for x, y, color in petals:
        draw.ellipse([x-5, y-6, x+5, y+6], fill=color, outline=None)
    
    # Draw center in gold
    draw.ellipse([13, 13, 19, 19], fill='#ffd700', outline=None)
    
    # Save as multiple sizes
    sizes = []
    for size in [16, 32, 48]:
        resized = img.resize((size, size), Image.Resampling.LANCZOS)
        sizes.append(resized)
    
    # Save as ICO with multiple sizes
    sizes[1].save(
        '/home/mustafa/telcobright-projects/orchestrix/orchestrix-ui/public/favicon.ico',
        format='ICO',
        sizes=[(16, 16), (32, 32), (48, 48)]
    )
    
    print("Favicon created successfully!")

if __name__ == "__main__":
    create_orchid_favicon()