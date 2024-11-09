package scalashop.image

import scalashop.common.*

import java.util.concurrent.ForkJoinPool
import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.ForkJoinTaskSupport

private def availableProcessors = sys.runtime.availableProcessors()

final class ParImage(
    src: Image,
    private var parallelization: Int = availableProcessors
) extends Image(src.height, src.width):

  private def buildSequential(
      destination: ArrayImage,
      xFrom: Int,
      xTo: Int,
      yFrom: Int,
      yTo: Int
  ): Unit =
    for
      y <- yFrom until yTo
      x <- xFrom until xTo
    do
      destination(x, y) = this(x, y)

  override def build: ArrayImage =
    // Compute the collection to work on
    val step = (height + parallelization - 1) / parallelization
    val splittingPoints = (0 to height by step).toList
    val ranges = splittingPoints.zip(splittingPoints.tail)
    val parRanges = ranges.par
    parRanges.tasksupport = ForkJoinTaskSupport(
      ForkJoinPool(parallelization)
    )

    val destination = new ArrayImage(height, width, new Array[Pixel](height * width))

    // perform your computation in parallel
    parRanges.foreach { case (yFrom, yTo) =>
      buildSequential(destination, 0, width, yFrom, yTo)
    }
    // return the constructed image
    destination

  def apply(x: Int, y: Int): Pixel = src(x, y)

  override def seq: Image = src.seq // recursively eliminate parallelization
  override def par: ParImage = par(availableProcessors)
  override def par(n: Int): ParImage =
    require(n >= 1)
    if n == parallelization then this
    else ParImage(src, n)
