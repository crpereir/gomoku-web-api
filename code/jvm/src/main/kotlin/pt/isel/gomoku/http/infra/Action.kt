package pt.isel.gomoku.http.infra

object Action {
    object Name {
        const val SIGNUP = "signup"
        const val LOGIN = "login"
        const val LOGOUT = "logout"

        const val START = "start-game"
        const val CANCEL_START = "cancel"
        const val PLAY = "play"
        const val GIVE_UP = "give-up"
    }

    object Title {
        const val SIGNUP = "Sign up"
        const val LOGIN = "Log in"
        const val LOGOUT = "Log out"

        const val START = "Start a game"
        const val CANCEL_START = "Cancel game start"
        const val PLAY = "Make a move"
        const val GIVE_UP = "Give up on game"
    }

    object Type {
        const val APPLICATION_JSON = "application/json"
    }

    object Field {
        const val USERNAME = "username"
        const val EMAIL = "email"
        const val PASSWORD = "password"
        const val IDENTITY = "identity"

        const val VARIANT_ID = "variantId"
    }
}
