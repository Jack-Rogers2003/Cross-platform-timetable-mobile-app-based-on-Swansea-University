import FirebaseAuth
import FirebaseDatabase

class UserInformation {
    static let shared = UserInformation()

    private var auth: Auth
    private var database: DatabaseReference = FirebaseDatabase.getInstance("https://dissertationapp-860fa-default-rtdb.europe-west1.firebasedatabase.app/")


    private init() {
        self.auth = Auth.auth()
        self.database = Database.database().reference()
    }

    func getCurrentUser() -> User? {
        return auth.currentUser
    }

    func getDatabaseReference() -> DatabaseReference {
        return database
    }
}