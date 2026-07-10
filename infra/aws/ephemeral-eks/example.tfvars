aws_region         = "ap-northeast-2"
cluster_name       = "highfivebooks-ephemeral"
kubernetes_version = null

node_instance_types = ["t3.medium"]
node_desired_size   = 1
node_min_size       = 1
node_max_size       = 2

# Keep false by default so image repositories are not accidentally deleted.
# Set true only when you intentionally want `terraform destroy` to delete images too.
force_delete_ecr = false
