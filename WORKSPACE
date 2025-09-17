workspace(name = "viaduct_test")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Rules Java
http_archive(
    name = "rules_java",
    urls = [
        "https://github.com/bazelbuild/rules_java/releases/download/7.6.5/rules_java-7.6.5.tar.gz",
    ],
    sha256 = "8afd053dd2a7b85a4f033584f30a7f1666c5492c56c76e04eec4428bdb2a86cf",
)

http_archive(
    name = "com_github_google_copybara",
    strip_prefix = "copybara-c4b8e73f260c55ac9e65db89423ae6ca1e798d24",
    url = "https://github.com/google/copybara/archive/c4b8e73f260c55ac9e65db89423ae6ca1e798d24.zip",
)

load("@com_github_google_copybara//:repositories.bzl", "copybara_repositories")
copybara_repositories()

load("@com_github_google_copybara//:repositories.maven.bzl", "copybara_maven_repositories")
copybara_maven_repositories()

load("@com_github_google_copybara//:repositories.go.bzl", "copybara_go_repositories")
copybara_go_repositories()