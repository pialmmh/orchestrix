# Images Directory

This directory contains container images and virtual machine templates for deployment.

#Main Workflow
lxc:
1. Claud code or any other AI agent will build LXD image of various container e.g. mysql/graphana as requested by the user
2. the name of the container is the immediate folder name
3. AI Agent will create a <container name>.yml file first
4. create a build script <container name>_build.sh that when executed will:
   a. create the container
   b. launch the container using the .yml file
   c. take backup of the container image in tar.gz file
   d. upload the file to a google drive account already mapped using rclone (repository config must be put in the same .yml )


## Directory Structure

```
images/
├── lxc/           # LXC container images
├── docker/        # Docker images
└── vms/           # Virtual machine images
```

## Storage Format

### LXC Images
- Format: `.tar.gz` or `.tar.xz`
- Example: `monitoring-stack-v2.45.0.tar.gz`
- Contains: rootfs + config

### Docker Images
- Format: `.tar` (docker save output)
- Example: `smsgateway-3.2.1.tar`
- Load with: `docker load < image.tar`

### VM Images
- Format: `.qcow2`, `.vmdk`, `.vdi`
- Example: `ubuntu-22.04-base.qcow2`
- For: KVM, VMware, VirtualBox

## Naming Convention

`{artifact-name}-v{version}.{extension}`

Examples:
- `monitoring-stack-v2.45.0.tar.gz`
- `smsgateway-v3.2.1.tar`
- `freeswitch-v1.10.9.qcow2`

## Note

Large image files should not be committed to git. Instead:
1. Store images in external repositories (Google Drive, S3, etc.)
2. Use `.gitignore` to exclude image files
3. Document image locations in artifact configurations