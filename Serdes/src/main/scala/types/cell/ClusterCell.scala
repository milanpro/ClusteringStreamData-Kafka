package types.cell

import types.point.Point

case class ClusterCell(
  seedPoint: Point,
  timelyDensity: Double,
  dependentDistance: Option[Double]
) extends Serializable
