package classes

/**@
* General compound object to store identifying characteristics of a compound, known properties,
* and functions to determine missing identification attributes.
*/
class Compound {

    String qID  // WikiData ID
    String pcID // pubchem ID
    String inChI
    String inChIShort
    String inChIKey
    String inChIKeyGenerated
    String smiles
    String workspaceRoot
    def molecule
    String formula
    String[] isomericSMILES
    Map bactingDetails
    Map compoundProperties

    GroovyShell shell = new GroovyShell()
    def tools = shell.parse(new File('tools/BactingUtils.groovy'))

    /**@
    * Create Compound object based on workspace Root (required for bacting) and InChIKey
    *
    * @param workspaceRoot String: A string for the root area of the workspace
    * @param inChIKey String: The known InChIKey for the compound
    * @return Compound Object
    */
    Compound(String workspaceRoot, String inChIKey, String smiles) {
        this.workspaceRoot = workspaceRoot
        this.inChIKey = inChIKey
        this.smiles = smiles
    }

    /**@
    * Display all attributes for the compounds for debugging
    */
    void displayDetails() {
        println '\n\n All details of compound: \n'
        println "WorkspaceRoot: ${this.workspaceRoot}"
        println "InChI: ${this.inChI}"
        println "InChIKey: ${this.inChIKey}"
        println 'IsomericSMILES: '
        this.displaySmiles()
        println "Main smiles: ${this.smiles}"
        println "Molecule: ${this.molecule}"
    }

    /**@
    * Check if Smiles notation is disconnected
    *
    * @return boolean: Whether the smiles for the compound is disconnected
    */
    boolean checkSmilesDisconnected() {
        return this.inChI.contains('.')
    }

    /**@
    * Update the compound's smiles isomeres based on the compound inChIKey
    */
    void updateIsomeresFromInChI() {
        this.isomericSMILES = tools.getIsomeresFromInChI(this.workspaceRoot, this.inChIKey)
        if (this.isomericSMILES.length > 1) {
            println 'Multiple isomeric smiles, selecting first instance as compound smiles'
        }
        this.smiles = this.isomericSMILES[0].trim()
    }
    /**@
    * Return the isomeres list for the compound
    * @return String[]: List of isometric smiles for the compound
    */
    String[] smilesIsomeres() {
        return this.isomericSMILES
    }

    /**@
    * Print out all the isometric smiles for the compound
    */
    void displaySmiles() {
        for (smiles in this.isomericSMILES) {
            println smiles
        }
    }

    /**@
    * TODO: Not Tested
    * Update the molecule (Type undefined) for the compound
    */
    void updateInChIDetailsFromSmiles() {
        inChIObj = tools.generateInChI(this.workspaceRoot, this.smiles)
        this.inChI = inChIObj.value
        this.inChIShort = inChIObj.value[6..-1]
        this.inChIKey = inChIObj.key
    }

    /**@
    * Helper function to get an InChIManager object
    */
    def generateInChIObject() {
        return tools.generateInChIObject(this.workspaceRoot, this.molecule)
    }

    /**@
    * Determine missing details for the compound based on bacting libaries
    *
    * @return Map: Hash map of details for the compound generated from the bacting libaries
    */
    Map bactingDetails() {
        this.bactingDetails = tools.bactingDetails(this.workspaceRoot, this.inChIKey)
        this.inChIKeyGenerated = this.bactingDetails.inChIKeyGenerated
        this.inChI = this.bactingDetails.inChI
        this.inChiShort = this.bactingDetails.inChiShort
        return this.bactingDetails
    }

    def completeIdentificationQuickStatement(){
        // check for missing properties
        sparql = """
        PREFIX wdt: <http://www.wikidata.org/prop/direct/>
        SELECT ?compound ?formula ?key ?inchi ?smiles ?pubchem WHERE {
            VALUES ?compound { <${existingQcode}> }
            OPTIONAL { ?compound wdt:$smilesProp ?smiles }
            OPTIONAL { ?compound wdt:P274 ?formula }
            OPTIONAL { ?compound wdt:P235 ?key }
            OPTIONAL { ?compound wdt:P234 ?inchi }
            OPTIONAL { ?compound wdt:P662 ?pubchem }
        }
        """
    }

    /**@
    * Should check if compound AND property exist prior to quick statment
    *
    *
    */
    def addPropertiesToQuickStatement(quickStatementFile, paperReference=null) {
        File file = new File(quickStatementFile)

        // Iterate through the hash map
        // Check some things while iterating
        // Does the inChiKey already exist in WikiData?
        // Does the property already exist?
        quickStatement = ""

        this.compoundProperties.each { key, val ->
            
            // Get the compound element from the first row of the results
            // pcExistingQcode = results.get(1, 'compound')


            file.append(this.pcID)


            // Set the paper reference
            if (paperReference != null) paperProv = "\tS248\t$paperReference"

            item = existingQcode.substring(32)
            pubchemLine = pubchemLine.replace('LAST', 'Q' + existingQcode.substring(32))

            classInfo = "Q$item\tP31\t$compoundClassQ"
            typeInfo = "Q$item\tP31\tQ11173"

            statement = """
            $classInfo$paperProv
            $typeInfo$paperProv\n"""

            "Q$item\t$smilesProp\t\"$smiles\"\n"
             if (smiles.length() <= 400) statement += "      Q$item\t$smilesProp\t\"$smiles\"\n"        

            if (idProperty != null && idProperty != "" && idProperty != "P662" && !extidFound) {
            statement += "      Q$item\t$idProperty\t\"$extid\"$paperProv\n"
            }

    ui.append(qsFile, statement + "\n")

    if (item == "LAST") {
      statement = """
      CREATE
      """
    } else statement = ""
   
    if (compoundClassQ != null) statement += "$item\tP31\t$compoundClassQ$paperProv\n"
   
    statement += """
      $item\tP31\tQ11173$paperProv
      $item\tDen\t\"chemical compound\"$paperProv
      $item\t$smilesProp\t\"$smiles\"
      $item\tP274\t\"$formula\"
    """
    if (name.length() > 0) statement += "  $item\tLen\t\"${name}\"\n    "
    if (inchiShort.length() <= 400) statement += "  $item\tP234\t\"$inchiShort\""
    statement += """
      $item\tP235\t\"$key\"
      $pubchemLine
    """

    if (idProperty != null && idProperty != "" && !pubchemLine.contains("P662")) {
      statement += "  $item\t$idProperty\t\"$extid\"$paperProv"
    }

    ui.append(qsFile, statement + "\n")

        }


    }

    boolean isCanonicalSmiles() {
        return !(
            this.smiles.contains('@') ||
            this.smiles.contains('/') ||
            this.smiles.contains('\\'))
    }

    void generateFormula() {
        this.formula = upgradeChemFormula(cdk.molecularFormula(this.molecule))
        this.modifyChemicalFormula()
    }
    
    void modifyChemicalFormula() {
        Map subscript_map = [
            '0': '₀',
            '1': '₁',
            '2': '₂',
            '3': '₃',
            '4': '₄',
            '5': '₅',
            '6': '₆',
            '7': '₇',
            '8': '₈',
            '9': '₉'
        ]
        for (entry in subscript_map) {
            this.formula = this.formula.replace(entry.key, entry.value)
        }
        this.formula = formula
    }

    void addAttributes() {
        undefinedCenters = cdk.getAtomsWithUndefinedStereo(mol)
        fullChiralityIsDefined = undefinedCenters.size() == 0
        ignoreBecauseStereoMissing =  options.s && !fullChiralityIsDefined
    }

}
