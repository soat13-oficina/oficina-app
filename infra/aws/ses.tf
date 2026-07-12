data "aws_iam_policy_document" "ses_send_email" {
  statement {
    sid       = "AllowSesSendEmail"
    actions   = ["ses:SendEmail"]
    resources = ["*"]
  }
}

resource "aws_iam_policy" "ses_send_email" {
  name        = "${var.cluster_name}-ses-send-email"
  description = "Permite apenas o envio de e-mail via SES (ses:SendEmail), usada pelo pod da API via IRSA."
  policy      = data.aws_iam_policy_document.ses_send_email.json
}

# Trust policy restrita ao service account "oficina-api" no namespace "oficina"
# (mesmo nome/namespace de k8s/00b-serviceaccount.yaml), seguindo o padrao IRSA:
# so esse service account pode assumir a role, e apenas via o OIDC provider
# deste cluster.
data "aws_iam_policy_document" "ses_role_trust" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [module.eks.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "${module.eks.oidc_provider}:sub"
      values   = ["system:serviceaccount:oficina:oficina-api"]
    }

    condition {
      test     = "StringEquals"
      variable = "${module.eks.oidc_provider}:aud"
      values   = ["sts.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ses_send_email" {
  name               = "${var.cluster_name}-ses-send-email"
  assume_role_policy = data.aws_iam_policy_document.ses_role_trust.json
}

resource "aws_iam_role_policy_attachment" "ses_send_email" {
  role       = aws_iam_role.ses_send_email.name
  policy_arn = aws_iam_policy.ses_send_email.arn
}
