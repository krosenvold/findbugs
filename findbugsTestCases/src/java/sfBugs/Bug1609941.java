package sfBugs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**

Bonjour,

je ne comprends pas le pb suivant :

"D�r�f�rencement imm�diat du r�sultat d'un readLine()
Le r�sultat d'un appel � readLine() est imm�diatement d�r�f�renc�. S'il n'y a plus d'autre lignes de texte � lire, readLine() retournera null ce qui provoquera une NullPointerException lors du d�r�f�rencement."

Concerne le code suivant :
BufferedReader lReader = new BufferedReader(new InputStreamReader(in));
if ("o".equals(lReader.readLine()))

Ou est le bug ?
Qu'entendez vous exactement par "d�r�f�rencement" ?

Merci pour vos r�ponses


 *
 */
public class Bug1609941 {
	boolean b(File f) throws IOException {
		BufferedReader in = new BufferedReader(new FileReader(f));
		boolean result = "o".equals(in.readLine());
		in.close();
		return result;
	}

}
