package fr.hurrycane.entity

case class KubernetesMember(address: String, status: String)

case class KubernetesMembers(kubernetesMember: List[KubernetesMember])