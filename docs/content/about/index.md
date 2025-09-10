---
title: About Viaduct
linkTitle: About
menu: {main: {weight: 10}}
---

{{% blocks/cover title="About Viaduct" image_anchor="bottom" height="auto" %}}
{{% /blocks/cover %}}

{{% blocks/section color="white" %}}

<div class="row justify-content-center mb-4">
<div class="col-md-7">
The Viaduct project was initially started in 2020 to address the complexity and
inefficiencies caused by an ever-growing dependency graph of microservices at Airbnb. It offers a data-oriented service mesh that provides a unified interface, based on GraphQL, for accessing and interacting with any data source while hosting business logic. This approach provides a single global schema, maintained by the teams that own the data, enabling reliable and consistent data access and mutations without each team having to implement logic for every query. Viaduct helps avoid a tangled microservices architecture by organizing service interactions around data rather than remote procedure calls, making data access more efficient and safe.
</div>
</div>

<div class="row justify-content-center mb-4">
<div class="col-md-7">
At the beginning of 2024, Airbnb began a rebuild of the original Viaduct
system to improve developer experience, modularity, and architectural integrity. Over time, the previous system had become complex, with multiple ways to implement functionality and weak abstraction boundaries, making it hard to evolve without disrupting users. The rebuild introduces a simplified and unified developer API and a strong modular structure through "tenant modules." It also creates clearer boundaries between the GraphQL execution engine, the tenant API, and hosted application code, enhancing maintainability and enabling easier evolution of each layer independently. This modernization has allowed Airbnb to scale Viaduct use significantly while reducing operational overhead and improving performance and reliability. The new design and architecture support gradual migration and increased developer productivity, benefiting Airbnb by centralizing business logic, reducing overhead, and improving the developer experience across hundreds of teams.
</div>
</div>

<div class="row justify-content-center">
<div class="col-md-7">
Airbnb open-sourced the rebuilt Viaduct in September 2025 to share its benefits
with the broader developer community.
</div>
</div>
{{% /blocks/section %}}
