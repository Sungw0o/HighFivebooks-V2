locals {
  name_prefix = "highfivebooks"

  common_tags = {
    Project   = "HighFiveBooks-V2"
    ManagedBy = "Terraform"
    Purpose   = "portfolio-ephemeral-evidence"
  }

  service_repositories = toset([
    "order-server",
    "book-server",
    "member-server",
    "coupon-server",
    "payment-server",
    "elasticsearch-nori"
  ])
}
