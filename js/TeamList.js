import React from 'react';

import { graphql, createFragmentContainer } from 'react-relay';

import PlayerDetails from './PlayerDetails';

const TeamList = createFragmentContainer(
	({data}) => (
			<ol>
				{data.map((team,i) => 
            <li key={i}>
						  {team.name}
							<ol>
							  {team.players.map((player, i) => <PlayerDetails key={i} data={player} />)}
							</ol>
						</li>
					)}
    	</ol>
	),
	graphql`fragment TeamList on Team @relay(plural: true) { id, name, players { ...PlayerDetails } }`
);

export default TeamList;
