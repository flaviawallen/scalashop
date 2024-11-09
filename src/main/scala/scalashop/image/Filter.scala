package scalashop.image

import scalashop.common.*

/** Identity filter, does not change pixels of the source image. */
class Identity(src: Image) extends Image(src.height, src.width):
  def apply(x: Int, y: Int): Pixel =
    src(x, y)

/** Grayscale filter, transforms the source image in a grayscale one. */
class Grayscale(src: Image) extends Image(src.height, src.width):
  // we generate a weighted grayscale image
  // to do this, we compute the "Luma" of each pixel
  // these numbers come from a standard called Rec 601
  // and are computed based on how we perceive colour and brightness
  // see: https://en.wikipedia.org/wiki/Luma_(video)
  val lumaR = 0.299f
  val lumaG = 0.587f
  val lumaB = 0.114f
  def grayscale(input: Pixel) =
    val a = alpha(input)
    val r = red(input).toFloat
    val g = green(input).toFloat
    val b = blue(input).toFloat
    val luma = (lumaR * r + lumaG * g + lumaB * b).toInt
    argb(a, luma, luma, luma)

  def apply(x: Int, y: Int): Pixel = grayscale(src(x, y))

class RedSplash(src: Image) extends Grayscale(src):
  def isRedEnough(px: Pixel) =
    val r = red(px).toFloat
    val g = green(px).toFloat
    val b = blue(px).toFloat
    (r > 1.7 * g) && (r > 1.7 * b)

  override def apply(x: Int, y: Int): Pixel =
    val px = src(x, y)
    if isRedEnough(px) then px else grayscale(px)

/** Performs a simple box-blur of given radius by averaging over a pixel's
  * neighbours
  *
  * @param src
  *   source image
  */
class SimpleBlur(src: Image) extends Image(src.height, src.width):
  val radius: Int = 3

  def apply(x: Int, y: Int): Pixel =
    var sumA = 0
    var sumR = 0
    var sumG = 0
    var sumB = 0
    var count = 0
    for
      dx <- -radius to radius
      dy <- -radius to radius
    do
      val nx = x + dx
      val ny = y + dy
      if nx >= 0 && nx < width && ny >= 0 && ny < height then
        val pixel = src(nx, ny)
        sumA += alpha(pixel)
        sumR += red(pixel)
        sumG += green(pixel)
        sumB += blue(pixel)
        count += 1
    val avgA = sumA / count
    val avgR = sumR / count
    val avgG = sumG / count
    val avgB = sumB / count
    argb(avgA, avgR, avgG, avgB)


/** Produce the convolution of an image with a kernel
  *
  * @param src
  *   source image
  * @param kernel
  *   kernel to convolve with
  */
class Convolution(src: Image, kernel: Kernel) extends Matrix[(Float, Float, Float, Float)]:
  val height: Int = src.height
  val width: Int = src.width

  def toImage =
    FloatMatrixImage(this)

  def apply(x: Int, y: Int): (Float, Float, Float, Float) =
    var sumA = 0.0f
    var sumR = 0.0f
    var sumG = 0.0f
    var sumB = 0.0f
    val rx = kernel.width / 2
    val ry = kernel.height / 2
    for
      i <- 0 until kernel.width
      j <- 0 until kernel.height
    do
      val dx = i - rx
      val dy = j - ry
      val nx = x + dx
      val ny = y + dy
      if nx >= 0 && nx < width && ny >= 0 && ny < height then
        val weight = kernel(i, j)
        val pixel = src(nx, ny)
        sumA += alpha(pixel).toFloat * weight
        sumR += red(pixel).toFloat * weight
        sumG += green(pixel).toFloat * weight
        sumB += blue(pixel).toFloat * weight
    (sumA, sumR, sumG, sumB)

/** Blur filter, computes a convolution between the image and the given blurring
  * kernel.
  */
class Blur(src: Image, kernel: Kernel) extends Image(src.height, src.width):
  private val convolution = Convolution(
    src,
    kernel.map(_ / kernel.sum)
  ).toImage // for blurring, kernels are normalized to have sum = 1
  def apply(x: Int, y: Int): Pixel = convolution(x, y)

/** Box blur filter, blur filter with matrix of size `(radius * 2 + 1) x (radius
  * * 2 + 1)` filled with ones.
  */
class BoxBlur(src: Image, radius: Int) extends Blur(src, Kernel.uniform(radius * 2 + 1))

/** Gaussian blur filter, blurs with a 3x3 Gaussian kernel. */
class GaussianBlur(src: Image) extends Blur(src, Kernel.gaussian3x3)
