val wordCounts = splits.map(x => (x, 1)).reduceByKey(_+_).updateStateByKey(updateFunc)
   val updateFunc = (values: Seq[Int], state: Option[Int]) => {
      val currentCount = values.foldLeft(0)(_ + _)
 
      val previousCount = state.getOrElse(0)
      val updatedSum = currentCount+previousCount
 
      Some(updatedSum)
 
    }