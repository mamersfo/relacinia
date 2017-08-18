import React from 'react';

import { graphql, createFragmentContainer } from 'react-relay';

import PlayerDetails from './PlayerDetails';

const TeamDetails = createFragmentContainer(
	({data}) => (
		<li key={data.id}>
		  {data.name}
		  <ol>
			{data.players.map((player, i) => <PlayerDetails key={i} data={player} />)}
		  </ol>
		</li>
	),
	graphql`fragment TeamDetails on Team { id, name, players { ...PlayerDetails } }`
);

export default TeamDetails;
