variable "aws_region" {
  description = "AWS region for ephemeral evidence."
  type        = string
  default     = "ap-northeast-2"
}

variable "cluster_name" {
  description = "Ephemeral EKS cluster name."
  type        = string
  default     = "highfivebooks-ephemeral"
}

variable "kubernetes_version" {
  description = "Optional EKS Kubernetes version. Leave null to use the AWS default supported version."
  type        = string
  default     = null
}

variable "node_instance_types" {
  description = "Free Tier eligible worker node instance types for the full MSA workload."
  type        = list(string)
  default     = ["t3.small"]
}

variable "node_desired_size" {
  description = "Desired worker node count for the MSA workloads."
  type        = number
  default     = 6
}

variable "node_min_size" {
  description = "Minimum worker node count."
  type        = number
  default     = 1
}

variable "node_max_size" {
  description = "Maximum worker node count."
  type        = number
  default     = 6
}

variable "ci_node_instance_types" {
  description = "Instance types for the isolated Jenkins build agent node group."
  type        = list(string)
  default     = ["t3.small"]
}

variable "search_node_instance_types" {
  description = "Instance types for the isolated Elasticsearch node group."
  type        = list(string)
  default     = ["t3.small"]
}

variable "force_delete_ecr" {
  description = "Allow Terraform destroy to delete non-empty ECR repositories."
  type        = bool
  default     = false
}
