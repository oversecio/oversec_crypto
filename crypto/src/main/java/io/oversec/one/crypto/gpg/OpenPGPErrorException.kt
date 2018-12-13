package io.oversec.one.crypto.gpg

import org.openintents.openpgp.OpenPgpError

class OpenPGPErrorException(val error: OpenPgpError) : Exception()
