# Infrastructure Tree Context Menu System

## Overview
The Infrastructure Tree in Orchestrix implements a hierarchical context menu system similar to Google Drive, where each tree node type has its own specific context menu actions accessible via right-click.

## Node Hierarchy & Context Actions

### 1. Infrastructure (Root Node)
- **Right-Click Actions**: None (label only)

### 2. Cloud Instances (Container Node)  
- **Right-Click Actions**:
  - ✅ Add New Cloud

### 3. Individual Cloud (e.g., "Telcobright")
- **Right-Click Actions**:
  - ✅ Add Datacenter
  - ✅ Edit Cloud
  - ✅ Delete Cloud

### 4. Datacenter Node
- **Right-Click Actions**:
  - ✅ Add Compute Node
  - ✅ Edit Datacenter
  - ✅ Delete Datacenter

### 5. Compute Node
- **Right-Click Actions**:
  - 🔄 Add Container (future)
  - ✅ Edit Compute Node
  - ✅ Delete Compute Node

### 6. Container Node (Future)
- **Right-Click Actions**:
  - 🔄 Start/Stop Container
  - 🔄 Edit Container
  - 🔄 Delete Container

## Implementation Details

### Context Menu Behavior
- **Trigger**: Right-click on any tree node
- **Position**: Menu appears at cursor position
- **Context-Aware**: Menu items change based on node type
- **Parent Reference**: When adding items, parent node is automatically linked

### Technical Implementation
```typescript
// Context menu state management
const [contextMenu, setContextMenu] = useState<{
  mouseX: number;
  mouseY: number;
  node: TreeNode | null;
} | null>(null);

// Handle right-click event
const handleContextMenu = (event: React.MouseEvent, node: TreeNode) => {
  event.preventDefault();
  setContextMenu({ mouseX: event.clientX, mouseY: event.clientY, node });
};
```

### Key Features
1. **Contextual Actions**: Each node type shows only relevant actions
2. **Parent-Child Relationships**: Automatically maintains hierarchy when adding items
3. **Visual Feedback**: Icons for each action type
4. **Separation**: Divider between modify and delete actions
5. **Consistent UX**: Similar to file management systems like Google Drive

## Benefits
- **Intuitive Navigation**: Users can manage infrastructure without leaving the tree view
- **Efficiency**: Quick access to common actions without navigating to buttons
- **Context Preservation**: Selected node context is maintained when performing actions
- **Scalability**: Easy to extend with new node types and actions

## Future Enhancements
- [ ] Keyboard shortcuts (e.g., Delete key, Ctrl+N for new)
- [ ] Drag and drop support for moving nodes
- [ ] Multi-select for bulk operations
- [ ] Copy/paste functionality for duplicating configurations
- [ ] Undo/redo support for actions