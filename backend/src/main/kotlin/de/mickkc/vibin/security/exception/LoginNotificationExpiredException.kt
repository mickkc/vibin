package de.mickkc.vibin.security.exception

class LoginNotificationExpiredException : Exception("This login notification's token is expired (too old).")