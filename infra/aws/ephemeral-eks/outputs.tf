output "cluster_name" {
  description = "EKS cluster name."
  value       = aws_eks_cluster.this.name
}

output "cluster_endpoint" {
  description = "EKS API endpoint."
  value       = aws_eks_cluster.this.endpoint
}

output "ecr_repository_urls" {
  description = "Service ECR repository URLs."
  value = {
    for name, repo in aws_ecr_repository.service : name => repo.repository_url
  }
}

output "update_kubeconfig_command" {
  description = "Command to configure kubectl for the ephemeral cluster."
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${aws_eks_cluster.this.name}"
}
