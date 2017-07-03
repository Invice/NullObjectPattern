package de.chw.sdg.db.transformer.relationship;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

public class GenericRelationshipTransformer {

	private static final String TYPE = "type";

	private final RelationshipType type;

	public GenericRelationshipTransformer(RelationshipType type) {
		this.type = type;
	}

	public Relationship transform(final Node sourceNode, final Node targetNode) {
		Relationship relationship = sourceNode.createRelationshipTo(targetNode, type);
		relationship.setProperty(TYPE, type.name());

		return relationship;
	}
}
