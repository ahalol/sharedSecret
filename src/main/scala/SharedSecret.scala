import scala.language.postfixOps
import org.netlib.util.intW
import scala.util.Random
import breeze.linalg._
import GF2Semirings._
import SharedSecret._

class SharedSecret(val numPeople: Int, val ofHowMany: Int) {
  val numRows = numPeople * 2
  val numCols = numRows + 2 /* which is origin */ + (ofHowMany - numPeople) * 2
  require(ofHowMany - numPeople < 5, "Not enough independent GF2 vectors")
  
  def create(s: String) = {
    val mat = growGF2IndependentMat(independentBaseGF2Mat)
    val shareMatrices = mat grouped 2 map (DenseMatrix.horzcat(_:_*).t)
    val genesisMatrix = shareMatrices next
    
    val secretVecs = str2Bits(s) grouped 2 map findKey(genesisMatrix, randomGF2Mat.toDenseVector) toList;
    (for (s <- shareMatrices) yield (secretVecs flatMap (s * _ toArray), s), genesisMatrix)
  }
  
  def findKey(mx: DenseMatGF2, can: DenseVecGF2)(vs: SeqGF2): DenseVecGF2 =
    if (DenseVector(vs:_*) == mx * can) can else findKey(mx, randomGF2Mat.toDenseVector)(vs)
  
  def independentBaseGF2Mat: ListOfDenseMatGF2 = {
    val listOfMats = Stream continually randomGF2Mat take 4 toList;
    if (this vecsAreIndependent listOfMats) listOfMats else independentBaseGF2Mat
  }
  
  def growGF2IndependentMat(vs: ListOfDenseMatGF2): ListOfDenseMatGF2 = 
    if (vs.size == numCols) vs else randomGF2Mat :: randomGF2Mat :: vs match { case newVecs => 
      val ok = newVecs.grouped(2).toList combinations(numPeople) map (_.flatten) forall vecsAreIndependent
      if (ok) growGF2IndependentMat(newVecs) else growGF2IndependentMat(vs)
    }
  
  // Two vectors u and v are linearly independent if the only numbers x and y satisfying xu + yv = 0 are x = y = 0.
  def vecsAreIndependent(vs: ListOfDenseMatGF2) = solveGF2(DenseMatrix.horzcat(vs:_*), DenseVector zeros numRows).size == 1
  def randomGF2Mat = DenseMatrix.zeros[GF2](numRows, 1) mapValues (v => if (Random.nextInt(100) < 50) v + One else v)
}

object SharedSecret {
  type ListOfDenseMatGF2 = List[DenseMatGF2]
  type DenseVecGF2 = DenseVector[GF2]
  type DenseMatGF2 = DenseMatrix[GF2]
  type SeqGF2 = Seq[GF2]
  
  def decode(genesisMatrix: DenseMatGF2, shares: (SeqGF2, DenseMatGF2)*) = {
    val vecs = for (v <- shares map { case (vs, _) => vs grouped 2 toList } transpose) yield DenseVector(v.flatten:_*)
    val checkMat: DenseMatGF2 = DenseMatrix.vertcat(shares map { case (_, mtx) => mtx }:_*)
    
    val solutions = for (v <- vecs) yield solveGF2(checkMat, v)
    solutions.flatten map (genesisMatrix * DenseVector(_:_*) toArray) flatten
  }
  
  // Factorial blowup :(
  // Should be replaced with QR-factorization
  def solveGF2(mx: DenseMatGF2, v: DenseVecGF2) = {
    def permute(n: Int) = Seq.fill(n)(One) ++ Seq.fill(mx.cols - n)(Zero) permutations;
    (0 to mx.cols).par flatMap permute filter (mx * DenseVector(_:_*) == v)
  }
  
  def apply(numPeople: Int, ofHowMany: Int) = new SharedSecret(numPeople, ofHowMany)
  val bits2Chars = (xs: SeqGF2) => for (v <- xs grouped 8) yield (v zip vs map { case (x, y) => x * y } sum).toChar
  val str2Bits = (s: String) => for (c <- s ; v <- vs ; val res = c & v) yield if (res > 0) One else Zero
  val vs = 0 until 8 map (1 << _)
}