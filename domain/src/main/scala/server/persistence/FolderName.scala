package medwork.server.persistence

/** Provides folder name, where object A should be stored
  * @tparam A
  *   stored object type
  */
type FolderName[F[_], A] = A => F[String]
