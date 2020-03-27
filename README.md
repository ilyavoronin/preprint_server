Arxiv data class description
--------------------------
* **identifier**(required) -- the unique identifier of an item in a repository
* **datestamp**(required) -- the date of creation, modification or deletion of the record for the purpose of selective harvesting.
* **specs**(optional) -- a list of [setSpec](http://www.openarchives.org/OAI/openarchivesprotocol.htm#Set) elements, which indicates the set membership of the item for the purpose of selective harvesting
* **creationDate**(requred) -- creation date
* **lastUpdateDate**(optional) -- the date when the record was last updated
* **title**(required) -- the title of the preprint
* **authors**(required) -- list of authors, where each author presented by Author data class. Author consists of two fields: **name**(required) and **affiliation**(optional) 
* **categories** -- the list of categories of the preprint. See the possible categories [here](https://arxiv.org/help/prep#subj)
* **comments**(optional) -- see the comments format [here](https://arxiv.org/help/prep#comments)
* **reportNo**(required only when supplied by author's institution) -- institution's locally assigned publication number
* **journalRef**(optional) --   full bibliographic reference if the article has already appeared in a journal or a proceedings
* **mscClass**(requred for math archives only) -- this field is used to indicate the mathematical classification code according to the [Mathematics Subject Classification](http://www.ams.org/msc/)
* **acmClass**(required for cs archives only) -- this field is used to indicate the classification code according to the [ACM Computing Classification System](https://www.acm.org/publications/computing-classification-system/1998)
* **doi**(optional) -- This field is only for a DOI ([Digital Object Identifier](http://doi.org/)) that resolves (links) to another version of the article, in a journal for example 
* **abstract**(requred)
* **license(?)
* **pdf** -- link to the pdf with preprint
* **refList** -- list of references