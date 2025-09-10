---
title: Viaduct
---

{{< blocks/cover title="Viaduct: A GraphQL Server" image_anchor="top" height="full" >}}
<a class="btn btn-lg btn-primary me-3 mb-4" href="docs/">
Learn More <i class="fas fa-arrow-alt-circle-right ms-2"></i>
</a>
<a class="btn btn-lg btn-secondary me-3 mb-4" href="https://github.com/airbnb/viaduct">
Download <i class="fab fa-github ms-2 "></i>
</a>
<p class="lead mt-5">A multitenant alternative to microservices</p>
{{< blocks/link-down color="info" >}}
{{< /blocks/cover >}}


{{% blocks/lead color="primary" %}}
Viaduct is a GraphQL-based system that provides a unified interface for accessing and interacting with any data source.

It can be used by services as a way to access data efficiently and safely.

Viaduct provides you with one global schema and query system. Regardless of the engineering ownership or backing service,
data access and mutations should follow a reliable and consistent pattern. The code which hydrates such queries should
be maintained by the team which owns the data, to prevent the need to implement such logic by every team querying it.

{{% pageinfo color="info" %}}
<i class="fa-solid fa-triangle-exclamation fa-sm"></i> The Viaduct engine is in production, at scale, at Airbnb where it has proven reliable. The developer API of Viaduct is under active development. In [our roadmap](roadmap) we indicate which parts of the API are more or less subject to future change.  This is a good time to join the project and influence the direction that this API takes!
{{% /pageinfo %}}
{{% /blocks/lead %}}

{{% blocks/section type="row justify-content-center" color="white" %}}

Core Values
{.h1 .text-center .mb-4}

{{% blocks/feature title="Be tenant-developer centric" icon="fas fa-building" %}}
Seek to understand tenant engineers' experiences, challenges and opportunities as a means to improve the frictions that exist today with the Viaduct framework.
{{% /blocks/feature %}}

{{% blocks/feature title="Be opinionated" icon="fas fa-message" %}}
Approach solutions to problems with an opinionated view as a means to create clarity for tenant developers over excessive choice and unnecessary complexity.
{{% /blocks/feature %}}

{{% /blocks/section %}}

{{% blocks/section type="row justify-content-center" color="white" %}}

{{% blocks/feature title="Deliver incrementally" icon="fas fa-signal" %}}
Start small and improve continuously through iteration on our technical solutions and processes. Ship incremental functionality on a frequent cadence as a means to build on an idea vs be stunted by perfection and a “solving for everything” mentality.
{{% /blocks/feature %}}

{{% blocks/feature title="Scale for the future" icon="fas fa-stairs" %}}
Build a system that can grow in reach and capability to more tenant engineers.
{{% /blocks/feature %}}

{{% /blocks/section %}}

{{% blocks/lead color="secondary" %}}
We envision Viaduct as the next-generation application runtime for product engineering teams.
To achieve this, we focus on our core mission to empower product developers by delivering a seamless and efficient developer experience. We strive to maximize productivity and spark creativity through simplified APIs, improved performance, and the cultivation of team autonomy.
{{% /blocks/lead %}}


{{% blocks/section color="white" type="row justify-content-center" class="trusted-by" %}}

Trusted By
{.h1 .text-center .mb-4}

{{% imgproc airbnb Resize "216x125" %}}
{{% /imgproc %}}

{{% /blocks/section %}}


{{% blocks/section type="row" color="primary" %}}
{{% blocks/feature icon="fab fa-github" title="Contributions welcome!"
url="https://github.com/airbnb/viaduct/blob/main/CONTRIBUTING.md" %}}
We use a [Pull Request](https://github.com/airbnb/viaduct/pulls) contributions workflow on **GitHub**. New users are always welcome!
{{% /blocks/feature %}}


{{% blocks/feature icon="fab fa-mastodon" title="Follow us on Mastodon!" url="https://mastodon.social/@AirbnbViaduct" %}}
For announcement of latest features etc.
{{% /blocks/feature %}}


{{% blocks/feature icon="fab fa-mastodon" title="Discussions" url="https://github.com/airbnb/viaduct/discussions/" %}}
Join the community to ask questions, share ideas, and discuss Viaduct-related topics.
{{% /blocks/feature %}}
{{% /blocks/section %}}
