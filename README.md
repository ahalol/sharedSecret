sharedSecret
============

Shared secret for N participants that can be unlocked by N - M of them.

`````scala
// EXAMPLE
// 4 participants in total. Any 3 of them can unlock a secret.
val secretGenerator = SharedSecret(numPeople = 3, ofHowMany = 4)
val (shares, genesisMat) = secretGenerator.create("There is no spoon")
    
// In this case <shares> is a list of 4 keys and corresponding generator vectors. 
// We give one of these keys to each of 4 participants. We can combine any 3 keys to obtain a secret. 
// Less than 3 keys won't do. Brute force selection isn't feasible due to extremely large number of possible variants.
val random3of4 = util.Random.shuffle(shares.toList combinations secretGenerator.numPeople) next;
val decodedBits = SharedSecret.decode(genesisMat, random3of4:_*)

// Printed out: There is no spoon
(bits2Char andThen (_.mkString) andThen println)(decodedBits)
`````
